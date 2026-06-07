from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    s = "hello"
    return s + 1
