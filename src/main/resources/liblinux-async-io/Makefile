CC=gcc --std=gnu99

ifndef JAVA_HOME
	@echo set defualt JAVA_HOME -> /usr/local/java/jdk1.8.0_65  
	JAVA_HOME := /usr/local/java/jdk1.8.0_65
endif

INCLUDES := -I ./include -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
SRCS := $(wildcard lib/*.c)
LIBS := 
TARGET := liblinux-async-io.so
LIB_PATH :=  -lpthread -lrt

OBJS := $(SRCS:.c=.o)
DEPS := $(OBJS:.o=.d)

#FLAG= -g $(INCLUDES)
CFLAGS = -g -fPIC -shared
LDFLAGS :=
LDFLAGS := -DPY_PROFILING
#LDFLAGS := -DPY_PROFILING  -DDEBUG_IO_TIME 
#LDFLAGS := -DPY_RDTSC

all:$(OBJS)	
	$(CC) $(CFLAGS) $(LDFLAGS) $(INCLUDES) $(OBJS) -o $(TARGET) $(LIB_PATH) $(LIBS)
	@echo $(TARGET) has been built successfully!

-include $(DEPS) 

objs:$(OBJS)
	@echo Compile protocol objects finished.

%.o:%.c
	$(CC) $(CFLAGS) $(LDFLAGS) $(INCLUDES) -c $< -o $@
	
%.d:%.c
	@set -e; rm -f $@; \
	$(CC) $(INCLUDES) $< -MM -MT $(@:.d=.o) >$@
#$(CC) -MM $(INCLUDES) $< > $@.$$$$; \
#sed 's,\($*\)\.o[ :]*,\1.o $@ : ,g' < $@.$$$$ > $@; \
#rm -f $@.$$$$

install:
	cp inc/*.h ../../my_inc
	cp *.so ../../my_lib

clean:
	rm -f $(OBJS) $(DEPS)
	rm -f $(TARGET)
