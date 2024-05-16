package grpcclientapp.observers;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import servicestubs.ImageUploadData;
import servicestubs.ImageUploadedData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageUploadedStreamObserver implements StreamObserver<ImageUploadedData> {

    private final ImageUploadData imageFile;

    public ImageUploadedStreamObserver(ImageUploadData imageFile) {
        this.imageFile = imageFile;
    }

    @Override
    public void onNext(ImageUploadedData imageUploadedData) {
        System.out.println("\nImage uploaded with id: " + imageUploadedData.getId());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("\nError uploading image: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.println("\nImage uploaded with name: <" + imageFile.getName() + "> was uploaded successfully");
    }

    public static ImageUploadData createImageFile(String imagePath) throws Exception {
        Path path = Paths.get(imagePath);
        // parse path to get file name (e.g. /path/to/image.jpg -> image)
        String fileName = path.getFileName().toString().split("\\.")[0];
        // get content type of the image (e.g. image/jpeg)
        String contentType = Files.probeContentType(path);
        // read file content
        ByteString data = ByteString.readFrom(Files.newInputStream(path));
        return ImageUploadData.newBuilder()
                .setName(fileName)
                .setContentType(contentType)
                .setData(data)
                .build();
    }

}
