from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    s = "hello"
    if s < 5:
        pass
    return "ok"
