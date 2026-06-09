from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    return "hello"
    x = 1
    return "world"
