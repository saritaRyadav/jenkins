const express = require("express");

const app = express();

app.get("/", (req, res) => {
    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>CloudBlitz DevOps CICD Integration Demo</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background: linear-gradient(to right, #0f172a, #1e3a8a);
                    color: white;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                }

                .container {
                    text-align: center;
                    background: rgba(255,255,255,0.1);
                    padding: 40px;
                    border-radius: 12px;
                    box-shadow: 0 0 20px rgba(0,0,0,0.3);
                    max-width: 700px;
                }

                h1 {
                    color: #38bdf8;
                    margin-bottom: 10px;
                }

                h2 {
                    color: #facc15;
                    margin-bottom: 20px;
                }

                p {
                    font-size: 18px;
                    line-height: 1.6;
                }

                .status {
                    display: inline-block;
                    margin-top: 20px;
                    padding: 10px 20px;
                    background: #22c55e;
                    color: white;
                    border-radius: 20px;
                    font-weight: bold;
                }

                footer {
                    margin-top: 25px;
                    font-size: 14px;
                    color: #d1d5db;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🚀 CloudBlitz DevOps CI/CD Integration Testing Demo</h1>
                <h2>Welcome to the Application!</h2>

                <p>
                    This application has been successfully deployed using an automated
                    <strong>CI/CD Pipeline</strong>.
                </p>

                <p>
                    <strong>GitHub → Jenkins → Docker → Deployment → Live Application Testing</strong>
                </p>

                <div class="status">
                    ✅ Latest Build Deployed Successfully
                </div>

                <footer>
                    Built with ❤️ using Node.js, Express, Docker & Jenkins
                </footer>
            </div>
        </body>
        </html>
    `);
});

app.get("/health", (req, res) => {
    res.status(200).json({
        status: "UP"
    });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`Application running on port ${PORT}`);
});

module.exports = app;