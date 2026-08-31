from flask import Flask, render_template
n = 5

@app.route('/d')
def d():
    w = 4
    return render_template('i.jinja')
