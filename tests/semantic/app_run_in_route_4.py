from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    app.run(debug=False)
    return render_template('i.jinja')
