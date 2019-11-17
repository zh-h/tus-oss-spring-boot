from tusclient import client

# Set Authorization headers if it is required
# by the tus server.
my_client = client.TusClient('http://localhost:8080/tus',
                              headers={})

# Set more headers.
my_client.set_headers({'Upload-FileId': 'test.txt'})

uploader = my_client.uploader('/Users/x/Downloads/iTerm2-3_3_6.zip', chunk_size= 2 * 1024 * 1024)

uploader.upload()
