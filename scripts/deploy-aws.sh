#!/usr/bin/env bash
# Ejecutar en AWS CloudShell. Crea recursos facturables en la cuenta actual.
set -euo pipefail
export AWS_PAGER=""
REGION="${AWS_REGION:-us-east-2}"
STACK="clinica-api-sofia"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE="$SCRIPT_DIR/../infra/aws.yaml"

if [[ ! -f "$TEMPLATE" ]]; then
  echo 'Ejecuta este script desde una copia completa del repositorio.' >&2
  exit 1
fi

echo "Region: $REGION. Stack: $STACK."
echo 'Se crearan una red publica, EC2 t3.small, disco EBS de 16 GiB y una IPv4 publica.'
echo 'Estos recursos pueden generar cargos segun los creditos de tu cuenta.'

aws cloudformation validate-template --region "$REGION" --template-body "file://$TEMPLATE" >/dev/null
if ! aws cloudformation deploy --region "$REGION" \
  --stack-name "$STACK" --template-file "$TEMPLATE" \
  --no-fail-on-empty-changeset; then
  echo 'No se completo el despliegue. Eventos recientes:' >&2
  aws cloudformation describe-stack-events --region "$REGION" --stack-name "$STACK" \
    --query 'StackEvents[0:12].[LogicalResourceId,ResourceStatus,ResourceStatusReason]' \
    --output table || true
  exit 1
fi

API_URL="$(aws cloudformation describe-stacks --region "$REGION" --stack-name "$STACK" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiUrl`].OutputValue' --output text)"
INSTANCE_ID="$(aws cloudformation describe-stacks --region "$REGION" --stack-name "$STACK" \
  --query 'Stacks[0].Outputs[?OutputKey==`InstanceId`].OutputValue' --output text)"

echo "Instancia creada: $INSTANCE_ID. Instalando Docker y construyendo la API..."
echo 'La primera construccion puede tardar varios minutos.'
for attempt in $(seq 1 120); do
  if curl --noproxy '*' -fsS --connect-timeout 3 --max-time 5 "$API_URL/actuator/health" 2>/dev/null \
    | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    echo
    echo 'DESPLIEGUE VERIFICADO: API y base de datos disponibles.'
    echo "ENLACE PARA EL PROFESOR: $API_URL"
    echo "SALUD: $API_URL/actuator/health"
    echo "PACIENTES: $API_URL/api/pacientes"
    echo "CITAS: $API_URL/api/citas"
    echo "Inicio verificado (UTC): $(date -u '+%Y-%m-%d %H:%M:%S')"
    echo 'Mantener la instancia encendida durante la revision. No se detiene automaticamente.'
    echo 'Para detenerla DESPUES de la revision (el disco se conserva y puede generar cargos):'
    echo "aws ec2 stop-instances --region $REGION --instance-ids $INSTANCE_ID"
    exit 0
  fi
  echo "Esperando el arranque ($attempt/120)..."
  sleep 10
done

echo "La instancia existe, pero la API aun no responde: $API_URL" >&2
echo 'No ejecutes otro despliegue. Comparte el mensaje y revisaremos el arranque.' >&2
echo 'Ultimas lineas del log de consola (si estan disponibles):' >&2
aws ec2 get-console-output --region "$REGION" --instance-id "$INSTANCE_ID" \
  --latest --query Output --output text | tail -n 60 || true
exit 1
