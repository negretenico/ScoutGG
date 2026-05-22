import os
import threading
from fastapi import FastAPI
from dotenv import load_dotenv

from consumer import start_consumer

load_dotenv()

app = FastAPI(title="scout-ai")


@app.on_event("startup")
def startup():
    thread = threading.Thread(target=start_consumer, daemon=True)
    thread.start()


@app.get("/health")
def health():
    return {"status": "ok"}
