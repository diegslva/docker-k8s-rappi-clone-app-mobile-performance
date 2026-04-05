$ErrorActionPreference = "SilentlyContinue"

$ComposeFile = "infra/docker/docker-compose.yml"
$ProjectPrefix = "rappiclone"

function Remove-RappiCloneContainers {
    docker ps -a --filter "name=$ProjectPrefix-" -q | ForEach-Object { docker rm -f $_ }
}

function Show-Help {
    Write-Host ""
    Write-Host "  RappiClone Dev Commands" -ForegroundColor Cyan
    Write-Host "  -----------------------"
    Write-Host "  up              Sobe toda infra"
    Write-Host "  down            Para toda infra"
    Write-Host "  restart         Reinicia toda infra"
    Write-Host "  nuke            Remove TUDO (containers + volumes)"
    Write-Host "  prepare-osrm    Prepara dados OSRM (executar uma vez)"
    Write-Host ""
}

switch ($args[0]) {
    "up" {
        Write-Host "[rappiclone] Parando containers existentes..." -ForegroundColor Yellow
        docker compose -f $ComposeFile down --remove-orphans 2>$null
        Remove-RappiCloneContainers
        Write-Host "[rappiclone] Subindo infra..." -ForegroundColor Green
        docker compose -f $ComposeFile up -d
        Write-Host ""
        Write-Host "[rappiclone] Infra pronta!" -ForegroundColor Green
        Write-Host "  Grafana:        http://localhost:3000  (admin/rappiclone_dev)"
        Write-Host "  Prometheus:     http://localhost:9090"
        Write-Host "  Kafka UI:       http://localhost:8180"
        Write-Host "  EMQX Dashboard: http://localhost:18083 (admin/rappiclone_dev)"
        Write-Host "  MinIO Console:  http://localhost:9001  (rappiclone/rappiclone_dev)"
        Write-Host "  Elasticsearch:  http://localhost:9200"
        Write-Host "  Nominatim:      http://localhost:8088"
        Write-Host "  ClickHouse:     http://localhost:8123"
        Write-Host ""
    }
    "down" {
        docker compose -f $ComposeFile down
    }
    "restart" {
        docker compose -f $ComposeFile down --remove-orphans 2>$null
        Remove-RappiCloneContainers
        docker compose -f $ComposeFile up -d
    }
    "nuke" {
        Write-Host "[rappiclone] Removendo TUDO (containers + volumes)..." -ForegroundColor Red
        docker compose -f $ComposeFile down -v --remove-orphans 2>$null
        Remove-RappiCloneContainers
        Write-Host "[rappiclone] Limpo." -ForegroundColor Green
    }
    "prepare-osrm" {
        Write-Host "[rappiclone] Preparando dados OSRM (regiao Sul do Brasil)..." -ForegroundColor Yellow
        Write-Host "[rappiclone] Isso pode demorar ~10 minutos na primeira vez." -ForegroundColor Yellow

        $OsrmDataDir = "infra/docker/osrm-data"
        if (-not (Test-Path $OsrmDataDir)) {
            New-Item -ItemType Directory -Path $OsrmDataDir -Force | Out-Null
        }

        # Download PBF se nao existir
        $PbfFile = "$OsrmDataDir/sul-latest.osm.pbf"
        if (-not (Test-Path $PbfFile)) {
            Write-Host "[rappiclone] Baixando dados OSM da regiao Sul..." -ForegroundColor Cyan
            Invoke-WebRequest -Uri "https://download.geofabrik.de/south-america/brazil/sul-latest.osm.pbf" -OutFile $PbfFile
        }

        # Pre-processar com OSRM
        Write-Host "[rappiclone] Extraindo..." -ForegroundColor Cyan
        docker run --rm -v "${PWD}/${OsrmDataDir}:/data" osrm/osrm-backend:v5.27.1 osrm-extract -p /opt/car.lua /data/sul-latest.osm.pbf

        Write-Host "[rappiclone] Particionando..." -ForegroundColor Cyan
        docker run --rm -v "${PWD}/${OsrmDataDir}:/data" osrm/osrm-backend:v5.27.1 osrm-partition /data/sul-latest.osrm

        Write-Host "[rappiclone] Customizando..." -ForegroundColor Cyan
        docker run --rm -v "${PWD}/${OsrmDataDir}:/data" osrm/osrm-backend:v5.27.1 osrm-customize /data/sul-latest.osrm

        Write-Host "[rappiclone] OSRM pronto! Use 'docker compose --profile geo up -d' pra subir o OSRM." -ForegroundColor Green
    }
    default {
        Show-Help
    }
}
