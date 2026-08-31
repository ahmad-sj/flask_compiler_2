from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

def pair(a, b):
    return b

@app.route('/d')
def d():
    x = pair(1, 2, 3)
    return render_template('i.jinja')
