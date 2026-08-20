# Recursion WITH a base case must not be reported as infinite.
from flask import Flask, render_template

app = Flask(__name__)

def countdown(n):
    if n <= 0:
        return 0
    return countdown(n - 1)

@app.route('/')
def index():
    return render_template('i.jinja', v=countdown(5))
