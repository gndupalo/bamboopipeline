FROM python:3.11-slim

WORKDIR /app

RUN python -m pip install --upgrade pip && \
    pip install checkov

RUN apt-get update && \
    apt-get install -y gnupg software-properties-common curl wget && \
    wget -O- https://apt.releases.hashicorp.com/gpg | \
    gpg --dearmor | \
    tee /usr/share/keyrings/hashicorp-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] \
    https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
    tee /etc/apt/sources.list.d/hashicorp.list && \
    apt-get update && apt-get install -y terraform

COPY . .

RUN chmod +x ./scripts/checkov.sh

CMD ["./scripts/checkov.sh"]  
