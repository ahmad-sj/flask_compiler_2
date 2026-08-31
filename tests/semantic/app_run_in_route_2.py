from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/a')
def a():
    app.run(debug=True)
    return render_template('i.jinja')
