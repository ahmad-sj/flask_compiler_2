from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    x = 0
    while x < 3:
        y = x + 1
        x = x + 1
    return y
