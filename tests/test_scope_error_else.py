from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    if False:
        pass
    else:
        result = "done"
    return result
