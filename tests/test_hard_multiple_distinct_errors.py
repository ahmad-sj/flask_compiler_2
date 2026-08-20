# Several unrelated faults in one file: the analyzer must report all of them,
# not stop at the first.
from flask import Flask, render_template

app = Flask(__name__)

items = [1, 2, 3]

def take_three(a, b, c):
    return a

@app.route('/')
def index():
    wrong_arity = take_three(1, 2)
    mismatch = "label" + 7
    ghost = never_declared
    return render_template('i.jinja', a=wrong_arity, b=mismatch, c=ghost)
    unreachable = 1
