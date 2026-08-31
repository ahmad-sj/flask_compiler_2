from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/p/<int:pid>')
def detail(pid):
    pid = 5
    return render_template('i.jinja')
