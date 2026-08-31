from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

def one(a):
    return a

@app.route('/b')
def b():
    x = one(1, 2)
    return render_template('i.jinja')
