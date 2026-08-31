from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def gamma():
    if True:
        w = 5
