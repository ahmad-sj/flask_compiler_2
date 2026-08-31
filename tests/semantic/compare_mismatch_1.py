from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def index():
    if 'a' < 1:
        x = 1
    return render_template('i.jinja')
