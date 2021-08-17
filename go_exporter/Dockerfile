FROM golang:alpine 
RUN mkdir /app 
ADD . /app/ 
WORKDIR /app 
RUN go build -o main .
EXPOSE 2112 
CMD ["/app/main"]
