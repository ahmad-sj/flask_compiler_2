from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/d')
def delta():
    n = 0
    while n < 3:
        n = n + 1
