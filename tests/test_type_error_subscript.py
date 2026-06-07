from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    x = 5
    return x[0]
