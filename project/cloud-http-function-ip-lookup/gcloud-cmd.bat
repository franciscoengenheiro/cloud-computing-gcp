gcloud functions deploy funcHttpHello --project=cn2324-t1-g04 --region=europe-west3-c --allow-unauthenticated --entry-point=functionhttp.Entrypoint --no-gen2 --runtime=java11 --trigger-http --source=target/deployment --service-account=cn-v2324-storage-g04@cn2324-t1-g04.iam.gserviceaccount.com --max-instances=3
