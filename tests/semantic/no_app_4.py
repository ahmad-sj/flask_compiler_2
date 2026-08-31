from flask import Flask, render_template
name = 'x'

@app.route('/c')
def c():
    z = 3
    return render_template('i.jinja')
