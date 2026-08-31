from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

def four(a, b, c, d):
    return a

@app.route('/c')
def c():
    x = four(1)
    return render_template('i.jinja')
