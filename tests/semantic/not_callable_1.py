from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def index():
    x = 1
    y = x()
    return render_template('i.jinja')
