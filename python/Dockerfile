FROM python:3.9.2-slim
WORKDIR /kiwi
COPY req.txt req.txt
RUN pip install -r req.txt
COPY kiwi kiwi
EXPOSE 5000
CMD ["flask", "--app", "kiwi", "run", "--host", "0.0.0.0"]
