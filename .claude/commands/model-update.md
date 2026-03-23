The goal of this command is to check for and update AI model API calls where new models can be released.

Do the following:
1. Assess all AI model API calls (namely we use VertexAI platform for text and image generation).
2. Check Vertex AI documentation for new models, available on vertex AI endpoints: https://docs.cloud.google.com/vertex-ai/generative-ai/docs/learn/overview
3. Report suggestions to user for manual review and approval, include brief summary of performance vs cost metrics and recommended updates.
4. Upon user instructions, update code invocations to new model(s)