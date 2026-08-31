from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/q/<int:n>')
def q(n):
    n = 'x'
    return render_template('i.jinja')
