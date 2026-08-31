from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/s/<int:k>')
def s(k):
    z = 1
    k = z
    return render_template('i.jinja')
