from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    for i in range(3):
        y = i + 1
    return y
