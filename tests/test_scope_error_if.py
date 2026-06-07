from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    if True:
        y = 10
    return y
