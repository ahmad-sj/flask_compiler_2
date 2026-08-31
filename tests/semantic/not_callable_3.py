from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    f = 2.5
    g = f()
    return render_template('i.jinja')
