from flask import Flask

app = Flask(__name__)

@app.route('/')
def index(a, b):
    return a + b

def caller():
    return index(1)
