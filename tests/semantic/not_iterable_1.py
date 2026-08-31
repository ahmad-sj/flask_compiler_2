from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def index():
    x = 5
    for i in x:
        y = i
    return render_template('i.jinja')
