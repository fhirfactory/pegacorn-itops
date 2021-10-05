# About 
ITOps Viewer. Allows for analysis of the subsystem.

## How to run the Application locally
1. In your terminal (Windows PowerShell on windows), navigate to the base directory of where the git repo has been checked out.
2. Install https://nodejs.org/en/download/
3. Run `npm install` to install all dependencies.
4. Run `npm start`

DEV ONLY
$env:PORT="443"
$env:HTTPS="true"

5. Open Notepad as an Admin and add to C:\Windows\System32\drivers\etc\hosts
127.0.0.1 itops-pm.site-a

6. Run `npm run start` to start the app.

7. Open localhost:3000 in a browser

# Docker image build details
1. In a Windows Command Prompt (due to the %date% %time% variable substitution - change this for other environments)
cd \dev\github\pegacorn-itops-pm 
docker build . --rm --no-cache --build-arg HTTP_PROXY=http://ACTGOV%5C[Your first name]%20[Your Surname]:[Your Password (URL encoded)]@proxy.test.act.gov.au:9090 --build-arg IMAGE_BUILD_TIMESTAMP="%date% %time%" -t pegacorn-itops-pm:1.0.0-snapshot
#docker run -p 3000:3000 pegacorn-itops-pm:1.0.0-snapshot

# NOTES

kubectl create secret generic pegacorn-itops-pm --from-literal=azureTenantId="b643c254-8bbf-4dac-94a8-40a13db11d38" --from-literal=azureClientId="6bb99238-db3b-4745-9901-7655e0b6f80a" --namespace=site-a

\helm\helm upgrade pegacorn-itops-pm-site-a --install --namespace site-a --set serviceName=pegacorn-itops-pm,basePort=30895,hostPath=/data,hostPathCerts=/data/certificates,imageTag=1.0.0-snapshot,numOfPods=1 helm
