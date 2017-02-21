//
//  SSOExtensionHelpItemViewController.m
//  Accenture SSO
//
//  Created by Joshua Lamkin on 9/29/15.
//  Copyright © 2015 Accenture. All rights reserved.
//

#import "SSOExtensionHelpViewController.h"
#import "SSOExtensionHelpItemViewController.h"
#import <UIKit/UIKit.h>

@interface SSOExtensionHelpViewController () <UIPageViewControllerDataSource, UIAlertViewDelegate>

@property (nonatomic, weak) UIPageViewController *pageViewController;
@property (nonatomic, weak) UILabel *dontShowAgainLabel;
@property (nonatomic, weak) UIImageView *dontShowAgainImageView;
@property (nonatomic) BOOL dontShowAgain;

@end

@implementation SSOExtensionHelpViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    SSOExtensionHelpItemViewController *startingViewController = [self viewControllerAtIndex:0];
    NSArray *viewControllers = @[startingViewController];
    [self.pageViewController setViewControllers:viewControllers direction:UIPageViewControllerNavigationDirectionForward animated:NO completion:nil];
    self.dontShowAgain = NO;
}

- (void)notShowAgainTapped {
    self.dontShowAgain = !self.dontShowAgain;
    [self processDontShowAgain];
}

- (void)processDontShowAgain {
    if (self.dontShowAgain) {
        self.dontShowAgainImageView.image = [UIImage imageNamed:@"slide-1-check-select.png"];
    } else {
        self.dontShowAgainImageView.image = [UIImage imageNamed:@"slide-1-check-empty.png"];
    }
}

- (SSOExtensionHelpItemViewController *)viewControllerAtIndex:(NSUInteger)index
{
    if (index >= 4) {
        return nil;
    }
    
    // Create a new view controller and pass suitable data.
    SSOExtensionHelpItemViewController *pageContentViewController;
    if (index == 0) {
        pageContentViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"Page1ContentViewController"];
        self.dontShowAgainImageView = [pageContentViewController.view viewWithTag:100];
        self.dontShowAgainLabel = [pageContentViewController.view viewWithTag:200];
        UITapGestureRecognizer *tapGestureRecgonizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(notShowAgainTapped)];
        tapGestureRecgonizer.numberOfTapsRequired = 1;
        [self.dontShowAgainLabel addGestureRecognizer:tapGestureRecgonizer];
        self.dontShowAgainLabel.userInteractionEnabled = YES;
        tapGestureRecgonizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(notShowAgainTapped)];
        tapGestureRecgonizer.numberOfTapsRequired = 1;
        [self.dontShowAgainImageView addGestureRecognizer:tapGestureRecgonizer];
        self.dontShowAgainImageView.userInteractionEnabled = YES;
        [self processDontShowAgain];
    } else if (index == 1) {
        pageContentViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"Page2ContentViewController"];
    } else if (index == 2) {
        pageContentViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"Page3ContentViewController"];
    } else if (index == 3) {
        pageContentViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"Page4ContentViewController"];
        UIButton *button = [pageContentViewController.view viewWithTag:200];
        [button addTarget:self
                   action:@selector(skipButtonTapped:)
         forControlEvents:UIControlEventTouchUpInside];
    }
    
    pageContentViewController.pageIndex = index;
    
    return pageContentViewController;
}

#pragma mark - Page View Controller Data Source

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController
{
    NSUInteger index = ((SSOExtensionHelpItemViewController*) viewController).pageIndex;
    
    [self.pageControl setCurrentPage:index];
    
    if ((index == 0) || (index == NSNotFound)) {
        return nil;
    }
    
    index--;
    
    return [self viewControllerAtIndex:index];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController
{
    NSUInteger index = ((SSOExtensionHelpItemViewController*) viewController).pageIndex;
    
    [self.pageControl setCurrentPage:index];
    
    if (index == NSNotFound) {
        return nil;
    }
    
    index++;
    if (index == 4) {
        return nil;
    }
    
    return [self viewControllerAtIndex:index];
}

- (NSInteger)presentationCountForPageViewController:(UIPageViewController *)pageViewController
{
    return 4;
}

- (void) prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    NSString * segueName = segue.identifier;
    if ([segueName isEqualToString: @"pageViewSegue"]) {
        self.pageViewController = (UIPageViewController *) [segue destinationViewController];
        self.pageViewController.dataSource = self;
        self.pageViewController.automaticallyAdjustsScrollViewInsets = NO;
        self.automaticallyAdjustsScrollViewInsets = NO;
    }
}

- (IBAction)skipButtonTapped:(id)sender {
    if(self.dontShowAgain)
    {
        if (self.delegate != nil) {
            [self.delegate neverShowAgain];
        }
    }
    
    if (self.delegate != nil) {
            [self.delegate showSignIn];
    }
    
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
