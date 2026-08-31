from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

def three(a, b, c):
    return a

@app.route('/a')
def a():
    x = three(1, 2)
    return render_template('i.jinja')
