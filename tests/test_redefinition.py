from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    x = 1
    x = 2
    return x
