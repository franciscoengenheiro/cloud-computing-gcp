gcloud functions deploy funcHttp --project=cn2324-t1-g04 --region=europe-west3 --allow-unauthenticated --entry-point=functionhttp.Entrypoint --no-gen2 --runtime=java11 --trigger-http --source=target --service-account=cn-v2324-storage-g04@cn2324-t1-g04.iam.gserviceaccount.com --max-instances=3
