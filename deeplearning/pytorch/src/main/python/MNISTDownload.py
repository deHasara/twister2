#  // Licensed under the Apache License, Version 2.0 (the "License");
#  // you may not use this file except in compliance with the License.
#  // You may obtain a copy of the License at
#  //
#  // http://www.apache.org/licenses/LICENSE-2.0
#  //
#  // Unless required by applicable law or agreed to in writing, software
#  // distributed under the License is distributed on an "AS IS" BASIS,
#  // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  // See the License for the specific language governing permissions and
#  // limitations under the License.

from twister2deepnet.deepnet.datasets.MNIST import MNIST

mnist_train = MNIST(source_dir='/tmp/twister2deepnet/mnist/train/', train=True, transform=None)

mnist_train.download()


mnist_test = MNIST(source_dir='/tmp/twister2deepnet/mnist/test/', train=False, transform=None)

mnist_test.download()