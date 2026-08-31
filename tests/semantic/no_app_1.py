from flask import Flask, render_template

@app.route('/')
def index():
    x = 1
    return render_template('i.jinja')
