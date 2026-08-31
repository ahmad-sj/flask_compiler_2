from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def gamma():
    y = 1
    return gamma()
