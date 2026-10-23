param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$pacienteId = $null
$citaIds = [System.Collections.Generic.List[long]]::new()

function Invoke-Api {
    param([string]$Method, [string]$Path, [int]$Expected, $Body = $null)
    $parameters = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        UseBasicParsing = $true
        TimeoutSec = 30
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 5))
    }
    try {
        $response = Invoke-WebRequest @parameters
        $statusCode = [int]$response.StatusCode
    }
    catch {
        if ($null -eq $_.Exception.Response) { throw }
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -ne $Expected) { throw }
        Write-Host "OK $Method $Path -> $statusCode"
        return $null
    }
    if ($statusCode -ne $Expected) {
        throw "$Method $Path esperaba $Expected y recibio $statusCode"
    }
    Write-Host "OK $Method $Path -> $statusCode"
    if ($response.Content) {
        $responseText = $response.Content
        if ($responseText -is [byte[]]) {
            $responseText = [System.Text.Encoding]::UTF8.GetString($responseText)
        }
        return ($responseText | ConvertFrom-Json)
    }
}

try {
    $health = Invoke-Api GET '/actuator/health' 200
    if ($health.status -ne 'UP') { throw 'El servicio no esta disponible.' }
    $patient = @{
        documento = ('test-' + [guid]::NewGuid().ToString('N').Substring(0, 20))
        nombres = 'Paciente de prueba'
        apellidos = 'Demo'
        fechaNacimiento = '2000-05-10'
        email = 'demo@example.com'
        telefono = '3001234567'
    }
    $created = Invoke-Api POST '/api/pacientes' 201 $patient
    $pacienteId = $created.id
    if (!$pacienteId) { throw 'El paciente creado no tiene ID.' }
    $null = Invoke-Api GET '/api/pacientes' 200
    $null = Invoke-Api GET "/api/pacientes/$pacienteId" 200
    $patient.nombres = 'Paciente actualizado'
    $updated = Invoke-Api PUT "/api/pacientes/$pacienteId" 200 $patient
    if ($updated.nombres -ne $patient.nombres) { throw 'No se actualizo el paciente.' }
    $null = Invoke-Api POST '/api/pacientes' 409 $patient

    $appointment = @{
        pacienteId = $pacienteId
        fechaHora = '2026-11-15T09:00:00'
        motivo = 'Control general'
        medico = 'Medico de prueba'
        estado = 'PROGRAMADA'
    }
    foreach ($index in 1..2) {
        $created = Invoke-Api POST '/api/citas' 201 $appointment
        if (!$created.id) { throw 'La cita creada no tiene ID.' }
        $citaIds.Add([long]$created.id)
    }
    $null = Invoke-Api GET '/api/citas' 200
    $null = Invoke-Api GET "/api/citas/$($citaIds[0])" 200
    $related = @(Invoke-Api GET "/api/pacientes/$pacienteId/citas" 200)
    if ($related.Count -ne 2) { throw 'No se conservaron las dos citas del paciente.' }
    foreach ($item in $related) {
        if ($item.pacienteId -ne $pacienteId) { throw 'Relacion paciente-cita incorrecta.' }
    }
    $null = Invoke-Api DELETE "/api/pacientes/$pacienteId" 409
    $appointment.estado = 'COMPLETADA'
    $updated = Invoke-Api PUT "/api/citas/$($citaIds[0])" 200 $appointment
    if ($updated.estado -ne 'COMPLETADA') { throw 'No se actualizo la cita.' }

    foreach ($citaId in $citaIds) {
        $null = Invoke-Api DELETE "/api/citas/$citaId" 204
        $null = Invoke-Api GET "/api/citas/$citaId" 404
    }
    $citaIds.Clear()
    $null = Invoke-Api DELETE "/api/pacientes/$pacienteId" 204
    $null = Invoke-Api GET "/api/pacientes/$pacienteId" 404
    $pacienteId = $null
    Write-Host 'Prueba completa: ambos CRUD, relacion 1:N y conflictos verificados.' -ForegroundColor Green
}
finally {
    foreach ($citaId in $citaIds) {
        try { $null = Invoke-Api DELETE "/api/citas/$citaId" 204 }
        catch { Write-Warning "No se pudo limpiar la cita $citaId" }
    }
    if ($pacienteId) {
        try { $null = Invoke-Api DELETE "/api/pacientes/$pacienteId" 204 }
        catch { Write-Warning "No se pudo limpiar el paciente $pacienteId" }
    }
}
