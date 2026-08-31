from flask import Flask, render_template
server = 1

@app.route('/a')
def a():
    x = 1
    return render_template('i.jinja')
