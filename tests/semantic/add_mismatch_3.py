from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    z = 'text' + 3.5
    return render_template('i.jinja')
