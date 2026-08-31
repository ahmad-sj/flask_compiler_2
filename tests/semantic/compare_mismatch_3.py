from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    if 'c' <= 3.5:
        z = 1
    return render_template('i.jinja')
