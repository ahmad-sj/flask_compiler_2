from flask import Flask, render_template
products = [1]

@app.route('/b')
def b():
    y = 2
    return render_template('i.jinja')
