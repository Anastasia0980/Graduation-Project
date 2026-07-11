FROM python:3.10

ENV PYTHONUNBUFFERED=1 \
    SDL_VIDEODRIVER=dummy \
    MPLBACKEND=Agg

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
        libgl1 \
        libglib2.0-0 \
        libsm6 \
        libxext6 \
        libxrender1 \
        libsdl2-2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY docker/battle/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

COPY scripts/ /app/
RUN mkdir -p /input /output

ENTRYPOINT ["python", "/app/docker_battle_runner.py"]
